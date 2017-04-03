package com.elytradev.teckle.common.block;

import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.TileItemTube;
import com.elytradev.teckle.common.tile.base.TileItemNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkEntryPoint;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class BlockFilter extends BlockContainer {

    public static PropertyDirection FACING = PropertyDirection.create("facing");
    public static PropertyBool TRIGGERED = PropertyBool.create("triggered");

    public BlockFilter(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setDefaultState(blockState.getBaseState());
        this.setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, BlockPistonBase.getFacing(meta)).withProperty(TRIGGERED, Boolean.valueOf((meta & 8) > 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();
        if (state.getValue(TRIGGERED).booleanValue()) {
            i |= 8;
        }

        return i;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileFilter();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isRemote)
            return;
        EnumFacing facing = state.getValue(FACING);
        TileFilter tileEntityFilter = (TileFilter) worldIn.getTileEntity(pos);
        TileEntity neighbour = worldIn.getTileEntity(pos.offset(facing));
        if (neighbour != null && neighbour instanceof TileItemTube) {
            TileItemTube tube = (TileItemTube) neighbour;
            tileEntityFilter.setNode(new WorldNetworkEntryPoint(tube.getNode().network, pos, facing));
            tube.getNode().network.registerNode(tileEntityFilter.getNode());
            System.out.println(tileEntityFilter + " Setting network to " + tube.getNode().network);
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = EnumFacing.getDirectionFromEntityLiving(pos, placer);

        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, direction).withProperty(TRIGGERED, false);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
        // Handles cleanup of endpoint nodes, or nodes that should have been removed but weren't.
        EnumFacing sideChanged = EnumFacing.DOWN;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing.getDirectionVec().equals(pos.subtract(neighbor))) {
                sideChanged = facing;
                break;
            }
        }

        TileFilter filter = (TileFilter) world.getTileEntity(pos);
        IBlockState state = world.getBlockState(pos);
        if (filter.getWorld().isRemote || !state.getValue(FACING).getOpposite().equals(sideChanged))
            return;

        TileEntity neighbourTile = world.getTileEntity(neighbor);
        if (filter.getNode() == null || filter.getNode().network == null) {
            if (neighbourTile != null && neighbourTile instanceof TileItemTube) {
                filter.setNode(new WorldNetworkEntryPoint(((TileItemTube) neighbourTile).getNode().network, pos, state.getValue(FACING)));
                ((TileItemTube) neighbourTile).getNode().network.registerNode(filter.getNode());
            }
        } else {
            if (neighbourTile == null || !(neighbourTile instanceof TileItemTube)) {
                filter.getNode().network.unregisterNodeAtPosition(pos);
                filter.setNode(null);
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (worldIn.isRemote)
            return;

        boolean powered = worldIn.isBlockPowered(pos);
        boolean hadPower = state.getValue(TRIGGERED);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileFilter) {
            if (powered) {
                worldIn.setBlockState(pos, state.withProperty(TRIGGERED, true), 4);
                if (!hadPower)
                    ((TileFilter) tileentity).pushToNetwork();
            } else {
                worldIn.setBlockState(pos, state.withProperty(TRIGGERED, false), 4);
            }
        }
    }

    @Override
    public int tickRate(World worldIn) {
        return 3;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.isRemote) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity != null && tileEntity instanceof TileFilter) {
                ((TileFilter) tileEntity).pushToNetwork();
            }
        }
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, TRIGGERED);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        if (tileAtPos != null) {
            TileItemNetworkMember networkMember = (TileItemNetworkMember) tileAtPos;
            if (networkMember.getNode() == null)
                return;
            networkMember.getNode().network.unregisterNodeAtPosition(pos);
            networkMember.getNode().network.validateNetwork();
            networkMember.setNode(null);
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote)
            return true;

        //TODO: Gui.

        //TileEntity tile = worldIn.getTileEntity(pos);
        //if (tile != null && tile instanceof TileFilter) {
        //    return ((TileFilter) tile).pushToNetwork();
        //}

        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}